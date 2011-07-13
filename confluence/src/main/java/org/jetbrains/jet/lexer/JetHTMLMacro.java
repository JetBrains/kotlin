package org.jetbrains.jet.lexer;

import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;

import java.util.Map;

/**
 * @author abreslav
 */
public class JetHTMLMacro extends BaseMacro {

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
        return code;
    }
}
