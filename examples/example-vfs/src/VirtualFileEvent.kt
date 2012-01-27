package org.jetbrains.jet.samples.vfs;

/**
 * Listener for receiving virtual file event
 */
public trait VirtualFileListener : java.util.EventListener {
    fun eventHappened(val event : VirtualFileEvent)
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
    override fun toString(): String? {
        return "created ${file}"
    }
}

/**
 * Event of deleting file
 */
public class VirtualFileDeletedEvent(file : VirtualFile) : VirtualFileEvent(file) {
    override fun toString(): String? {
        return "deleted ${file}"
    }
}

/**
 * Event of changing file contents
 */
public class VirtualFileChangedEvent(file : VirtualFile) : VirtualFileEvent(file) {
    override fun toString(): String? {
        return "changed ${file}"
    }
}
