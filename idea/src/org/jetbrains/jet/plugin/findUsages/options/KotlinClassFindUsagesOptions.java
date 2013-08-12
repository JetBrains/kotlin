package org.jetbrains.jet.plugin.findUsages.options;

import com.intellij.find.findUsages.JavaClassFindUsagesOptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class KotlinClassFindUsagesOptions extends JavaClassFindUsagesOptions {
    public KotlinClassFindUsagesOptions(@NotNull Project project) {
        super(project);
    }
}
