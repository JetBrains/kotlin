package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.PositionManagerFactory;
import com.intellij.debugger.engine.DebugProcess;

/**
 * @author yole
 */
public class JetPositionManagerFactory implements PositionManagerFactory {
    @Override
    public PositionManager create(DebugProcess debugProcess) {
        return new JetPositionManager(debugProcess);
    }
}
