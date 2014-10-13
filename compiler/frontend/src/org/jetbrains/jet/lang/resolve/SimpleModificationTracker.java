package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.util.ModificationTracker;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

//NOTE: copied to support changes depending on IDEA 14 branch
public class SimpleModificationTracker implements ModificationTracker {
    public volatile int myCounter;

    @Override
    public long getModificationCount() {
        return myCounter;
    }

    private static final AtomicIntegerFieldUpdater<SimpleModificationTracker> UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SimpleModificationTracker.class, "myCounter");

    public void incModificationCount() {
        UPDATER.incrementAndGet(this);
    }
}