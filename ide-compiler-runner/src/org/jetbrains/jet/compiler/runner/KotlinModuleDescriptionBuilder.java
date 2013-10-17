package org.jetbrains.jet.compiler.runner;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface KotlinModuleDescriptionBuilder {
    KotlinModuleDescriptionBuilder addModule(
            String moduleName,
            String outputDir,
            DependencyProvider dependencyProvider,
            List<File> sourceFiles,
            boolean tests,
            Set<File> directoriesToFilterOut);

    CharSequence asText();

    interface DependencyProvider {
        void processClassPath(DependencyProcessor processor);
    }

    interface DependencyProcessor {
        void processClassPathSection(String sectionDescription, Collection<File> files);
        void processAnnotationRoots(List<File> files);
    }
}
