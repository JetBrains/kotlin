package org.jetbrains.jet.plugin.caches;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Krasko
 */
public class JetCacheManager implements ProjectComponent {
    private Project myProject;
    private JetShortNamesCache myCache;

    public static JetCacheManager getInstance(Project project) {
        return project.getComponent(JetCacheManager.class);
    }

    public JetCacheManager(Project project) {
        myProject = project;
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {

    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Kotlin caches manager";
    }

    @Override
    public void initComponent() {
        myCache = new JetShortNamesCache(myProject);
    }

    @Override
    public void disposeComponent() {

    }

    public JetShortNamesCache getNamesCache() {
        return myCache;
    }
}
