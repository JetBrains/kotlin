package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.PositionManagerFactory;
import com.intellij.debugger.engine.DebugProcess;

/**
 * @author yole
 */
public class JetPositionManagerFactory extends PositionManagerFactory {
    @Override
    public PositionManager createPositionManager(DebugProcess process) {
        return new JetPositionManager(process);
    }
}
