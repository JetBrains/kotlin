package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.CommonBundle;
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModelImpl;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportUtil;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportOptionsComponent;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

// Copy of com.intellij.framework.addSupport.impl.AddSupportForSingleFrameworkDialog with additional write lock on model updating
// Class should be removed after fix for IDEA is ready.
public class AddSupportForSingleFrameworkDialogFixed extends DialogWrapper {
    private final Module myModule;
    private final FrameworkSupportInModuleConfigurable myConfigurable;
    private final FrameworkSupportModelBase myModel;
    private final FrameworkSupportOptionsComponent myComponent;
    private final FrameworkTypeEx myFrameworkType;
    private final ModifiableModelsProvider myModifiableModelsProvider;

    public AddSupportForSingleFrameworkDialogFixed(
            @NotNull Module module,
            final @NotNull String contentRootPath,
            FrameworkTypeEx frameworkType, @NotNull FrameworkSupportInModuleProvider provider,
            @NotNull LibrariesContainer librariesContainer,
            ModifiableModelsProvider modifiableModelsProvider
    ) {
        super(module.getProject(), true);
        myFrameworkType = frameworkType;
        myModifiableModelsProvider = modifiableModelsProvider;
        setTitle(ProjectBundle.message("dialog.title.add.framework.0.support", frameworkType.getPresentableName()));
        myModule = module;
        myModel = new FrameworkSupportModelImpl(module.getProject(), contentRootPath, librariesContainer);
        myConfigurable = provider.createConfigurable(myModel);
        myComponent = new FrameworkSupportOptionsComponent(myModel, myModel.getLibrariesContainer(), myDisposable, myConfigurable, null);
        Disposer.register(myDisposable, myConfigurable);
        init();
    }

    public static AddSupportForSingleFrameworkDialogFixed createDialog(
            @NotNull Module module,
            @NotNull FrameworkSupportInModuleProvider provider) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        if (roots.length == 0) return null;

        List<FrameworkSupportInModuleProvider> providers = FrameworkSupportUtil.getProviders(module, DefaultFacetsProvider.INSTANCE);
        if (providers.isEmpty()) return null;

        final LibrariesContainer container = LibrariesContainerFactory.createContainer(module.getProject());
        final IdeaModifiableModelsProvider modifiableModelsProvider = new IdeaModifiableModelsProvider();

        return new AddSupportForSingleFrameworkDialogFixed(module, roots[0].getPath(), provider.getFrameworkType(), provider, container, modifiableModelsProvider);
    }

    @Override
    protected void doOKAction() {
        final LibraryCompositionSettings librarySettings = myComponent.getLibraryCompositionSettings();
        if (librarySettings != null) {
            final ModifiableRootModel modifiableModel = myModifiableModelsProvider.getModuleModifiableModel(myModule);
            if (!askAndRemoveDuplicatedLibraryEntry(modifiableModel, librarySettings.getLibraryDescription())) {
                if (myConfigurable.isOnlyLibraryAdded()) {
                    myModifiableModelsProvider.disposeModuleModifiableModel(modifiableModel);
                    return;
                }
                return;
            }

            // Fix
            new WriteAction() {
                @Override
                protected void run(final Result result) {
                    myModifiableModelsProvider.commitModuleModifiableModel(modifiableModel);
                }
            }.execute();

            final boolean downloaded = librarySettings.downloadFiles(getRootPane());
            if (!downloaded) {
                int answer = Messages.showYesNoDialog(getRootPane(),
                                                      ProjectBundle.message("warning.message.some.required.libraries.wasn.t.downloaded"),
                                                      CommonBundle.getWarningTitle(), Messages.getWarningIcon());
                if (answer != 0) {
                    return;
                }
            }
        }

        new WriteAction() {
            @Override
            protected void run(final Result result) {
                final ModifiableRootModel rootModel = myModifiableModelsProvider.getModuleModifiableModel(myModule);
                if (librarySettings != null) {
                    librarySettings.addLibraries(rootModel, new ArrayList<Library>(), myModel.getLibrariesContainer());
                }
                myConfigurable.addSupport(myModule, rootModel, myModifiableModelsProvider);
                myModifiableModelsProvider.commitModuleModifiableModel(rootModel);
            }
        }.execute();
        super.doOKAction();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#com.intellij.framework.addSupport.AddSupportForSingleFrameworkDialogFixed";
    }

    @Override
    protected String getHelpId() {
        return "reference.frameworks.support.dialog";//todo[nik]
    }

    @Override
    protected JComponent createCenterPanel() {
        return myComponent.getMainPanel();
    }

    private boolean askAndRemoveDuplicatedLibraryEntry(@NotNull ModifiableRootModel rootModel, @NotNull CustomLibraryDescription description) {
        List<OrderEntry> existingEntries = new ArrayList<OrderEntry>();
        final LibrariesContainer container = myModel.getLibrariesContainer();
        for (OrderEntry entry : rootModel.getOrderEntries()) {
            if (!(entry instanceof LibraryOrderEntry)) continue;
            final Library library = ((LibraryOrderEntry)entry).getLibrary();
            if (library == null) continue;

            if (LibraryPresentationManager.getInstance().isLibraryOfKind(library, container, description.getSuitableLibraryKinds())) {
                existingEntries.add(entry);
            }
        }

        if (!existingEntries.isEmpty()) {
            String message;
            if (existingEntries.size() > 1) {
                message = "There are already " + existingEntries.size() + " " + myFrameworkType.getPresentableName() + " libraries.\n Do you want to replace they?";
            }
            else {
                final String name = existingEntries.get(0).getPresentableName();
                message = "There is already a " + myFrameworkType.getPresentableName() + " library '" + name + "'.\n Do you want to replace it?";
            }
            final int result = Messages.showYesNoCancelDialog(rootModel.getProject(), message, "Library Already Exists",
                                                              "&Replace", "&Add", "&Cancel", null);
            if (result == 0) {
                for (OrderEntry entry : existingEntries) {
                    rootModel.removeOrderEntry(entry);
                }
            }
            else if (result != 1) {
                return false;
            }
        }
        return true;
    }
}