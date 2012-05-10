package org.jetbrains.jet.samples.vfs;

/**
 * Listener for receiving virtual file event
 */
public trait VirtualFileListener : java.util.EventListener {
    fun eventHappened(val event : VirtualFileEvent)
}

// FIXME using this wrapper because of codegen bug KT-1737, should be replaced with
private class SimpleVirtualFileListener(val listenerFunction : (VirtualFileEvent)->Unit) : VirtualFileListener {
    override fun eventHappened(event : VirtualFileEvent) {
        val f = listenerFunction // FIXME saving to local variable because of the bug in codegen (KT-1739)
        f(event)
    }
}

/**
 * Base type of virtual file events
 * @property file affected by event
 */
public abstract class VirtualFileEvent(val file : VirtualFile) : Object() {
}

/**
 * Event of creating file
 */
public class VirtualFileCreateEvent(file : VirtualFile) : VirtualFileEvent(file) {
    override public fun toString(): String? {
        return "created ${file}"
    }
}

/**
 * Event of deleting file
 */
public class VirtualFileDeletedEvent(file : VirtualFile) : VirtualFileEvent(file) {
    override public fun toString(): String? {
        return "deleted ${file}"
    }
}

/**
 * Event of changing file contents
 */
public class VirtualFileChangedEvent(file : VirtualFile) : VirtualFileEvent(file) {
    override public fun toString(): String? {
        return "changed ${file}"
    }
}
