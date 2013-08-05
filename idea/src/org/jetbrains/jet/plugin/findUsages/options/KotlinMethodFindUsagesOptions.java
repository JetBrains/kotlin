package org.jetbrains.jet.plugin.findUsages.options;

import com.intellij.find.findUsages.JavaMethodFindUsagesOptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class KotlinMethodFindUsagesOptions extends JavaMethodFindUsagesOptions {
    public KotlinMethodFindUsagesOptions(@NotNull Project project) {
        super(project);
    }
}
