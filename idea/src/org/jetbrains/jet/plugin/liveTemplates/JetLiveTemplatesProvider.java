package org.jetbrains.jet.plugin.liveTemplates;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;

/**
 * @author Evgeny Gerashchenko
 * @since 1/27/12
 */
public class JetLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
    @Override
    public String[] getDefaultLiveTemplateFiles() {
        return new String[]{"liveTemplates/Kotlin"};
    }

    @Override
    public String[] getHiddenLiveTemplateFiles() {
        return new String[0];
    }
}
