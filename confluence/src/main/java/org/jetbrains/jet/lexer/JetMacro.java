package org.jetbrains.jet.lexer;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.apache.velocity.VelocityContext;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class JetMacro extends BaseMacro {
    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public RenderMode getBodyRenderMode() {
        return RenderMode.allow(0);
    }

    @Override
    public String execute(Map map, String code, RenderContext renderContext) throws MacroException {

        class ExtraTag {
            final String name;
            final String message;
            final int start;
            int end;

            ExtraTag(String name, String message, int start) {
                this.name = name;
                this.message = message;
                this.start = start;
            }
        }

        String[] knownExtraTagNames = {"error", "warning", "unresolved"};

        Map<String, Pattern> openTags = new HashMap<String, Pattern>();
        Map<String, Pattern> closeTags = new HashMap<String, Pattern>();
        for (String name : knownExtraTagNames) {
            Pattern open = Pattern.compile("<" + name + "\\s*(desc=\\\"([^\n\"]*?)\\\")?>", Pattern.MULTILINE);
            openTags.put(name, open);
            closeTags.put(name, Pattern.compile("</" + name + ">"));
        }

        Stack<ExtraTag> tagStack = new Stack<ExtraTag>();
        List<ExtraTag> tags = new ArrayList<ExtraTag>();

        code = code.trim();
        StringBuilder normalizedNewlines = new StringBuilder();
        charLoop:
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '\r') continue;
            for (String tagName : knownExtraTagNames) {
                Pattern open = openTags.get(tagName);
                CharSequence remainingInput = code.subSequence(i, code.length());
                Matcher openMatcher = open.matcher(remainingInput);
                if (openMatcher.find() && openMatcher.start() == 0) {
                    tagStack.push(new ExtraTag(tagName, openMatcher.group(2), normalizedNewlines.length()));
                    i += openMatcher.end() - 1;
                    continue charLoop;
                }

                Pattern close = closeTags.get(tagName);
                Matcher closeMatcher = close.matcher(remainingInput);
                if (closeMatcher.find() && closeMatcher.start() == 0) {
                    if (tagStack.isEmpty()) {
                        return "<div style=\"jet error\">Error: unmatched closing tag: " + closeMatcher.group() + "</div>";
                    }
                    else {
                        ExtraTag tag = tagStack.pop();
                        if (!tagName.equals(tag.name)) {
                            return "<div style=\"jet error\">Error: unmatched closing tag: " + closeMatcher.group() + "</div>";
                        }

                        tag.end = normalizedNewlines.length();
                        tags.add(tag);
                        i += closeMatcher.end() - 1;
                        continue charLoop;
                    }
                }

            }
            normalizedNewlines.append(c);
        }

        VelocityContext context = new VelocityContext(MacroUtils.defaultVelocityContext());
        String renderedTemplate = VelocityUtils.getRenderedTemplate("template.velocity", context);

        StringBuilder result = new StringBuilder(renderedTemplate);
        result.append(
                "<div class=\"code panel\" style=\"border-width: 1px;\">" +
                        "<div class=\"codeContent panelContent\">" +
                        "<div class=\"container\">"
        );

        _JetLexer jetLexer = new _JetLexer(new StringReader(""));
        jetLexer.reset(normalizedNewlines, 0, normalizedNewlines.length(), _JetLexer.YYINITIAL);

        Map<IElementType, String> styleMap = new HashMap<IElementType, String>();
        styleMap.put(JetTokens.BLOCK_COMMENT, "comment");
        styleMap.put(JetTokens.DOC_COMMENT, "comment");
        styleMap.put(JetTokens.EOL_COMMENT, "comment");
        styleMap.put(JetTokens.WHITE_SPACE, "whitespace");
        styleMap.put(JetTokens.INTEGER_LITERAL, "number");
        styleMap.put(JetTokens.FLOAT_LITERAL, "number");
        styleMap.put(JetTokens.OPEN_QUOTE, "string");
        styleMap.put(JetTokens.REGULAR_STRING_PART, "string");
        styleMap.put(JetTokens.ESCAPE_SEQUENCE, "escape");
        styleMap.put(JetTokens.LONG_TEMPLATE_ENTRY_START, "escape");
        styleMap.put(JetTokens.LONG_TEMPLATE_ENTRY_END, "escape");
        styleMap.put(JetTokens.SHORT_TEMPLATE_ENTRY_START, "escape");
        styleMap.put(JetTokens.ESCAPE_SEQUENCE, "escape");
        styleMap.put(JetTokens.CLOSING_QUOTE, "string");
        styleMap.put(JetTokens.CHARACTER_LITERAL, "string");
        styleMap.put(JetTokens.LABEL_IDENTIFIER, "label");
        styleMap.put(JetTokens.ATAT, "label");
        styleMap.put(JetTokens.FIELD_IDENTIFIER, "field");
        styleMap.put(JetTokens.RAW_STRING_LITERAL, "string");
        styleMap.put(TokenType.BAD_CHARACTER, "bad");

        Iterator<ExtraTag> iterator = tags.iterator();
        ExtraTag tag = iterator.hasNext() ? iterator.next() : null;
        while (true) {
            try {
                if (tag != null) {
                    if (tag.start == jetLexer.getTokenEnd()) {
                        result.append("<div class=\"jet ").append(tag.name).append("\"");
                        if (tag.message != null) {
                            result.append(" title=\"").append(tag.message).append("\"");
                        }
                        result.append(">");
                    }
                    else if (tag.end == jetLexer.getTokenEnd()) {
                        result.append("</div>");
                        tag = iterator.hasNext() ? iterator.next() : null;
                    }
                }
                IElementType token = jetLexer.advance();
                if (token == null) break;
                CharSequence yytext = jetLexer.yytext();
                String style = null;
                if (token instanceof JetKeywordToken) {
                    style = "keyword";
                } else if (token == JetTokens.IDENTIFIER) {
                    for (IElementType softKeyword : JetTokens.SOFT_KEYWORDS.asSet()) {
                        if (((JetKeywordToken) softKeyword).getValue().equals(yytext.toString())) {
                            style = "softkeyword";
                            break;
                        }
                    }
                    style = style == null ? "plain" : style;
                } else if (styleMap.containsKey(token)) {
                    style = styleMap.get(token);
                } else {
                    style = "plain";
                }
                result.append("<code class=\"jet ").append(style).append("\">").append(yytext).append("</code>");

            } catch (Throwable e) {
                result.append("ERROR IN HIGHLIGHTER: ").append(e.getClass().getSimpleName()).append(" : ").append(e.getMessage());
            }
        }

        result.append("</div>");
        result.append("</div>");
        result.append("</div>");
        return result.toString();
    }
}
