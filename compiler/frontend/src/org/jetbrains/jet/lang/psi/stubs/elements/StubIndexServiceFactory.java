package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.openapi.components.ServiceManager;

/**
 * @author Nikolay Krasko
 */
final class StubIndexServiceFactory {

    private StubIndexServiceFactory() {
    }

    public static StubIndexService getInstance() {
        // If executed in plugin service will be registered
        final StubIndexService registeredService = ServiceManager.getService(StubIndexService.class);
        return registeredService != null ? registeredService : StubIndexService.NO_INDEX_SERVICE;
    }
}
