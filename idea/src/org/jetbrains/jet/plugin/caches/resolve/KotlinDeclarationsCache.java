package org.jetbrains.jet.plugin.caches.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;

public interface KotlinDeclarationsCache {
    @NotNull
    BindingContext getBindingContext();

    @Nullable
    BodiesResolveContext getBodiesResolveContext();

    @NotNull @Deprecated // ModuleConfiguration must be obtained from the module
    ModuleConfiguration getModuleConfiguration();
}
