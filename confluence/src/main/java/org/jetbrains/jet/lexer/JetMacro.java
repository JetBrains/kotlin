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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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
        code = code.trim();
        VelocityContext context = new VelocityContext(MacroUtils.defaultVelocityContext());
        String renderedTemplate = VelocityUtils.getRenderedTemplate("template.velocity", context);

        StringBuilder result = new StringBuilder(renderedTemplate);
        result.append(
                "<div class=\"code panel\" style=\"border-width: 1px;\">" +
                        "<div class=\"codeContent panelContent\">" +
                        "<div class=\"container\">"
        );

        _JetLexer jetLexer = new _JetLexer(new StringReader(""));
        jetLexer.reset(code, 0, code.length(), _JetLexer.YYINITIAL);

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

        while (true) {
            try {
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

            } catch (IOException e) {
                result.append("ERROR IN HIGHLIGHTER: ").append(e.getClass().getSimpleName()).append(" : ").append(e.getMessage());
            }
        }

        result.append("</div>");
        result.append("</div>");
        result.append("</div>");
        return result.toString();
    }
}
