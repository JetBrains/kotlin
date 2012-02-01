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
            FileSystem.addWatchedDirectory(FileSystem.getFileByPath(arg))
        }
    }

    // add listener which prints out everything
    FileSystem.addVirtualFileListener{ event ->
            println(event)
            if (event is VirtualFileChangedEvent) {
                println("new file size is ${event.file.size()}")
            }
    }

    // wait for 1 minute
    Thread.sleep(60000)
}