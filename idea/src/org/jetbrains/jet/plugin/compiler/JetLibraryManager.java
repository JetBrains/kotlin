package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.ModuleLibraryTable;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.AbstractCompileEnvironment;

import java.io.File;

/**
 * @author alex.tkachman
 */
public class JetLibraryManager implements ModuleComponent {
    public static final String KOTLIN_STD_LIB = "KotlinStdLib";
    private Module module;
    private Library kotlin;

    public JetLibraryManager(Module module) {
        this.module = module;
    }
    
    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void moduleAdded() {
        if(ApplicationManager.getApplication().isUnitTestMode())
            return;

        final LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());
        kotlin = ApplicationManager.getApplication().runReadAction(new Computable<Library>() {
            @Override
            public Library compute() {
                return libraryTable.getLibraryByName(KOTLIN_STD_LIB);
            }
        });
        if(kotlin == null) {
            kotlin = ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
              @Override
              public Library compute() {
                  Library library = libraryTable.createLibrary(KOTLIN_STD_LIB);
                  Library.ModifiableModel modifiableModel = library.getModifiableModel();
                  final File unpackedRuntimePath = AbstractCompileEnvironment.getUnpackedRuntimePath();
                  if (unpackedRuntimePath != null) {
                      modifiableModel.addRoot(LocalFileSystem.getInstance().findFileByIoFile(unpackedRuntimePath), OrderRootType.CLASSES);
                  }
                  else {
                      final File runtimeJarPath = AbstractCompileEnvironment.getRuntimeJarPath();
                      if (runtimeJarPath != null && runtimeJarPath.exists()) {
                          modifiableModel.addRoot(LocalFileSystem.getInstance().findFileByIoFile(unpackedRuntimePath), OrderRootType.CLASSES);
                      }
                      else {
                          // todo
                      }
                  }
                  modifiableModel.commit();
                  return library;
              }
            });
        }

        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        final ModifiableRootModel modifiableModel = rootManager.getModifiableModel();
        LibraryOrderEntry libraryOrderEntry = modifiableModel.findLibraryOrderEntry(kotlin);
        if(libraryOrderEntry == null) {
            modifiableModel.addLibraryEntry(kotlin);
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    modifiableModel.commit();
                }
            });
        }
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return JetLibraryManager.class.getCanonicalName();
    }
}
