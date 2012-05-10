package org.jetbrains.jet.samples.vfs.sandbox;

import org.jetbrains.jet.samples.vfs.*;
import java.io.InputStreamReader
import java.util.Scanner

/**
 * Receives list of directories which should be watched, adds them to virtual file system,
 * adds virtual file system listener and waits for 1 minutes, printing out all received event.
 */
fun main(args : Array<String>) {
    if (args.size == 0) {
        println("Provide list of watched directories as command line arguments")
        return
    }

    // add watched directory
    FileSystem.write {
        for (arg in args) {
            val virtualFile = FileSystem.getFileByPath(arg)
            FileSystem.addWatchedDirectory(virtualFile)
        }
    }

    // add listener which prints out everything
    FileSystem.addVirtualFileListener{ event ->
            println(event)
            if (event is VirtualFileChangedEvent) {
                // FIXME explicit type casting to avoid overload ambiguity (KT-1461)
                println("new file size is ${(event as VirtualFileChangedEvent).file.size}")
            }
    }

    // wait for 1 minute
    Thread.sleep(60000)
}