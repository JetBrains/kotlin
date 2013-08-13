package org.jetbrains.jet.plugin.findUsages.options;

import com.intellij.find.findUsages.JavaClassFindUsagesOptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class KotlinClassFindUsagesOptions extends JavaClassFindUsagesOptions {
    public boolean searchConstructorUsages = true;

    public KotlinClassFindUsagesOptions(@NotNull Project project) {
        super(project);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
               && o instanceof KotlinClassFindUsagesOptions
               && ((KotlinClassFindUsagesOptions) o).searchConstructorUsages == searchConstructorUsages;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (searchConstructorUsages ? 1 : 0);
        return result;
    }
}
