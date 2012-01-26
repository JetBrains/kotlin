/*
 * @author max
 */
package org.jetbrains.jet.plugin.compiler;

import com.intellij.util.PathUtil;

import java.io.File;

public class CompilerUtil {
    public static File getDefaultCompilerPath() {
        File plugin_jar_path = new File(PathUtil.getJarPathForClass(CompilerUtil.class));
        
        if (!plugin_jar_path.exists()) return null;
        
        File lib = plugin_jar_path.getParentFile();
        File pluginHome = lib.getParentFile();
        
        File answer = new File(pluginHome, "kotlinc");

        return answer.exists() ? answer : null;
    }
    
    public static File getDefaultRuntimePath() {
        File compilerPath = getDefaultCompilerPath();
        if (compilerPath == null) return null;
        
        File answer = new File(compilerPath, "lib/kotlin-runtime.jar");

        return answer.exists() ? answer : null;
    }
}
