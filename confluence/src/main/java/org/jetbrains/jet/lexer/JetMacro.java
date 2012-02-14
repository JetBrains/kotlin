/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public static final StringReader DUMMY_READER = new StringReader("");

    private static class TagData {
        final TagType type;
        final String message;
        final int start;
        final boolean nextToken;
        int end;

        TagData(TagType type, String message, int start, boolean nextToken) {
            this.type = type;
            this.message = message;
            this.start = start;
            this.nextToken = nextToken;
        }
    }

    private static abstract class TagType {
        public final String tagName;

        private TagType(String tagName) {
            this.tagName = tagName;
        }

        public abstract void appendOpenTag(StringBuilder builder, TagData tagData);
        public abstract void appendCloseTag(StringBuilder builder, TagData tagData);

        @Override
        public String toString() {
            return tagName;
        }
    }

    private static class StyledDivTagType extends TagType {

        private StyledDivTagType(String tagName) {
            super(tagName);
        }

        @Override
        public void appendOpenTag(StringBuilder builder, TagData tagData) {
            assert tagData.type == this;
            builder.append("<div class=\"jet ").append(tagName).append("\"");
            if (tagData.message != null) {
                builder.append(" title=\"");
                escapeHTML(builder, tagData.message);
                builder.append("\"");
            }
            builder.append(">");
        }

        @Override
        public void appendCloseTag(StringBuilder builder, TagData tagData) {
            builder.append("</div>");
        }
    }

    private static final TagType[] knownExtraTagTypes = {
            new StyledDivTagType("error"),
            new StyledDivTagType("warning"),
            new StyledDivTagType("unresolved"),
            new TagType("ref") {
                @Override
                public void appendOpenTag(StringBuilder builder, TagData tagData) {
                    builder.append("<a class=\"jet ref\" href=\"#");
                    builder.append(tagData.message);
                    builder.append("\">");
                }

                @Override
                public void appendCloseTag(StringBuilder builder, TagData tagData) {
                    builder.append("</a>");
                }
            },
            new TagType("label") {
                @Override
                public void appendOpenTag(StringBuilder builder, TagData tagData) {
                    builder.append("<a name=\"");
                    builder.append(tagData.message);
                    builder.append("\">");
                }

                @Override
                public void appendCloseTag(StringBuilder builder, TagData tagData) {
                    builder.append("</a>");
                }
            },
            new TagType("a") {
                @Override
                public void appendOpenTag(StringBuilder builder, TagData tagData) {
                    builder.append("<a class=\"jet anchor\" href=\"");
                    builder.append(tagData.message);
                    builder.append("\">");
                }

                @Override
                public void appendCloseTag(StringBuilder builder, TagData tagData) {
                    builder.append("</a>");
                }
            },
            new TagType("style") {
                @Override
                public void appendOpenTag(StringBuilder builder, TagData tagData) {
                    builder.append("<div style=\"");
                    builder.append(tagData.message);
                    builder.append("\">");
                }

                @Override
                public void appendCloseTag(StringBuilder builder, TagData tagData) {
                    builder.append("</div>");
                }
            },
            new TagType("class") {
                @Override
                public void appendOpenTag(StringBuilder builder, TagData tagData) {
                    builder.append("<div class=\"");
                    builder.append(tagData.message);
                    builder.append("\">");
                }

                @Override
                public void appendCloseTag(StringBuilder builder, TagData tagData) {
                    builder.append("</div>");
                }
            },
    };
    private static final Map<TagType, Pattern> openTags = new HashMap<TagType, Pattern>();
    private static final Map<TagType, Pattern> nextTokenTags = new HashMap<TagType, Pattern>();
    private static final Map<TagType, Pattern> closedTags = new HashMap<TagType, Pattern>();
    private static final Map<TagType, Pattern> closeTags = new HashMap<TagType, Pattern>();
    static {
        for (TagType type : knownExtraTagTypes) {
            String tagName = type.tagName;
            openTags.put(type, Pattern.compile("<" + tagName + "\\s*((desc)?=\\\"([^\n\"]*?)\\\")?>", Pattern.MULTILINE));
            closeTags.put(type, Pattern.compile("</" + tagName + ">"));

            nextTokenTags.put(type, Pattern.compile("<" + tagName + "\\s*((desc)?=\\\"([^\n\"]*?)\\\")?:>", Pattern.MULTILINE));
            closedTags.put(type, Pattern.compile("<" + tagName + "\\s*((desc)?=\\\"([^\n\"]*?)\\\")?/>", Pattern.MULTILINE));
        }
    }

    private static final Map<IElementType, String> styleMap = new HashMap<IElementType, String>();
    static {
        styleMap.put(JetTokens.BLOCK_COMMENT, "jet-comment");
        styleMap.put(JetTokens.DOC_COMMENT, "jet-comment");
        styleMap.put(JetTokens.EOL_COMMENT, "jet-comment");
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
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public RenderMode getBodyRenderMode() {
        return RenderMode.allow(0);
    }

    private static void escapeHTML(StringBuilder builder, CharSequence seq) {
        if (seq == null) return;
        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '&':
                    builder.append("&amp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                default:
                    builder.append(c);
            }
        }
    }

    @Override
    public String execute(Map map, String code, RenderContext renderContext) throws MacroException {
        try {
            List<TagData> tags = new ArrayList<TagData>();

            StringBuilder afterPreprocessing = preprocess(code.trim(), tags);

            VelocityContext context = new VelocityContext(MacroUtils.defaultVelocityContext());
            String renderedTemplate = VelocityUtils.getRenderedTemplate("template.velocity", context);

            StringBuilder result = new StringBuilder(renderedTemplate);
            result.append(
                    "<div class=\"code panel\" style=\"border-width: 1px;\">" +
                            "<div class=\"codeContent panelContent\">" +
                            "<div class=\"container\">"
            );

            _JetLexer jetLexer = new _JetLexer(DUMMY_READER);
            jetLexer.reset(afterPreprocessing, 0, afterPreprocessing.length(), _JetLexer.YYINITIAL);

            Iterator<TagData> iterator = tags.iterator();
            TagData tag = iterator.hasNext() ? iterator.next() : null;
            while (true) {
                int tokenEnd = jetLexer.getTokenEnd();
                while (tag != null && tag.end < tokenEnd) {
                    result.append("<div class=\"jet hwarning\">Skipping a tag in the middle of a token: &lt;").append(tag.type).append("&gt;</div>");
                    tag = iterator.hasNext() ? iterator.next() : null;
                }

                if (tag != null) {
                    if (tag.start == tokenEnd) {
//                        result.append("<div class=\"jet ").append(tag.type).append("\"");
//                        if (tag.message != null) {
//                            result.append(" title=\"").append(tag.message).append("\"");
//                        }
//                        result.append(">");
                        tag.type.appendOpenTag(result, tag);
                    }
                }
                if (tag != null) {
                    if (tag.end == tokenEnd || (tag.nextToken && tag.start < tokenEnd)) {
                        tag.type.appendCloseTag(result, tag);
                        tag = iterator.hasNext() ? iterator.next() : null;
                    }
                }

                IElementType token = jetLexer.advance();
                if (token == null) break;
//                CharSequence yytext = jetLexer.yytext();
                String yytext = jetLexer.yytext().toString().replaceAll("\n", "\r\n");
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
                result.append("<code class=\"jet ").append(style).append("\">");
                escapeHTML(result, yytext);
                result.append("</code>");
            }

            result.append("</div>");
            result.append("</div>");
            result.append("</div>");
            return result.toString();
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<div class=\"jet herror\">Jet highlighter error [").append(e.getClass().getSimpleName()).append("]: ");
            escapeHTML(stringBuilder, e.getMessage());
            stringBuilder.append("<br/>");
            stringBuilder.append("Original text:");
            stringBuilder.append("<pre>");
            escapeHTML(stringBuilder, code);
            stringBuilder.append("</pre>");
            stringBuilder.append("</div>");
            return stringBuilder.toString();
        }
    }

    private StringBuilder preprocess(CharSequence code, Collection<TagData> tags) {
        StringBuilder afterPreprocessing = new StringBuilder();
        int initialLength = afterPreprocessing.length();
        Stack<TagData> tagStack = new Stack<TagData>();
        charLoop:
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '\r') continue;

            int position = afterPreprocessing.length() - initialLength;

            for (TagType type : knownExtraTagTypes) {
                Pattern open = openTags.get(type);
                Matcher openMatcher = matchFrom(code, i, open);
                if (openMatcher != null) {
                    tagStack.push(new TagData(type, openMatcher.group(3), position, false));
                    i += openMatcher.end() - 1;
                    continue charLoop;
                }

                Pattern close = closeTags.get(type);
                Matcher closeMatcher = matchFrom(code, i, close);
                if (closeMatcher != null) {
                    if (tagStack.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched closing tag: " + closeMatcher.group());
                    }
                    else {
                        TagData tag = tagStack.pop();
                        if (type != tag.type) {
                            throw new IllegalArgumentException("Unmatched closing tag: " + closeMatcher.group());
                        }

                        tag.end = position;
                        tags.add(tag);
                        i += closeMatcher.end() - 1;
                        continue charLoop;
                    }
                }

                Pattern closed = closedTags.get(type);
                Matcher closedMatcher = matchFrom(code, i, closed);
                if (closedMatcher != null) {
                    TagData tag = new TagData(type, closedMatcher.group(3), position, false);
                    tag.end = position;
                    tags.add(tag);
                    i += closedMatcher.end() - 1;
                    continue charLoop;
                }

                Pattern next = nextTokenTags.get(type);
                Matcher nextMatcher = matchFrom(code, i, next);
                if (nextMatcher != null) {
                    TagData tag = new TagData(type, nextMatcher.group(3), position, true);
                    tags.add(tag);
                    tag.end = code.length();
                    i += nextMatcher.end() - 1;
                    continue charLoop;
                }

            }
            afterPreprocessing.append(c);
        }
        return afterPreprocessing;
    }

    private Matcher matchFrom(CharSequence code, int start, Pattern pattern) {
        CharSequence remainingInput = code.subSequence(start, code.length());
        Matcher matcher = pattern.matcher(remainingInput);
        if (matcher.find() && matcher.start() == 0) {
            return matcher;
        }
        return null;
    }
}
