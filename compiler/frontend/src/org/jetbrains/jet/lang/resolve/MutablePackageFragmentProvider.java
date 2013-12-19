package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MutablePackageFragmentProvider implements PackageFragmentProvider {
    private final ModuleDescriptor module;

    private final Map<FqName, MutablePackageFragmentDescriptor> fqNameToPackage = Maps.newHashMap();
    private final MultiMap<FqName, FqName> subPackages = MultiMap.create();

    public MutablePackageFragmentProvider(@NotNull ModuleDescriptor module) {
        this.module = module;
        fqNameToPackage.put(FqName.ROOT, new MutablePackageFragmentDescriptor(module, FqName.ROOT));
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        return ContainerUtil.<PackageFragmentDescriptor>createMaybeSingletonList(fqNameToPackage.get(fqName));
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        return subPackages.get(fqName);
    }

    @NotNull
    public MutablePackageFragmentDescriptor getOrCreateFragment(@NotNull FqName fqName) {
        if (!fqNameToPackage.containsKey(fqName)) {
            FqName parent = fqName.parent();
            getOrCreateFragment(parent); // assure that parent exists

            fqNameToPackage.put(fqName, new MutablePackageFragmentDescriptor(module, fqName));
            subPackages.putValue(parent, fqName);
        }

        return fqNameToPackage.get(fqName);
    }

    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    @NotNull
    public Collection<MutablePackageFragmentDescriptor> getAllFragments() {
        return fqNameToPackage.values();
    }
}
