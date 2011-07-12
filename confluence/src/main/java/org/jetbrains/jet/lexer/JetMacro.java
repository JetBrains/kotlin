package org.jetbrains.jet.lexer;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.intellij.psi.tree.IElementType;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.StringReader;
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
        VelocityContext context = new VelocityContext(MacroUtils.defaultVelocityContext());
        String renderedTemplate = VelocityUtils.getRenderedTemplate("template.velocity", context);

        StringBuilder result = new StringBuilder(renderedTemplate);
        result.append(
                "<div class=\"code panel\" style=\"border-width: 1px;\">" +
                        "<div class=\"codeContent panelContent\">" +
                        "<div class=\"container\">"
        );

        _JetLexer jetLexer = new _JetLexer(new StringReader(code));
        jetLexer.reset(code, 0, code.length(), _JetLexer.YYINITIAL);
        while (true) {
            try {
                IElementType token = jetLexer.advance();
                if (token == null) break;
                CharSequence yytext = jetLexer.yytext();
                String style;
                if (token instanceof JetKeywordToken) {
                    style = "keyword";
                } else if (token == JetTokens.BLOCK_COMMENT || token == JetTokens.DOC_COMMENT || token == JetTokens.EOL_COMMENT) {
                    style = "comment";
                } else if (token == JetTokens.IDENTIFIER) {

                    style = "softkeyword";
                } else if (token == JetTokens.WHITE_SPACE) {
                    style = "whitespace";
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
