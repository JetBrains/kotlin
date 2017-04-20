/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.test.clientserver

import org.jetbrains.kotlin.codegen.getBoxMethodOrNull
import org.jetbrains.kotlin.codegen.getGeneratedClass
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Executors

//Use only JDK 1.6 compatible api
object TestProcessServer {

    val executor = Executors.newFixedThreadPool(1)!!

    @JvmStatic
    fun main(args: Array<String>) {
        val portNumber = args[0].toInt()
        println("Starting server on port $portNumber...")

        val serverSocket = ServerSocket(portNumber)
        try {
            while (true) {
                val clientSocket = serverSocket.accept()
                println("Socket established...")
                executor.execute(ServerTest(clientSocket))
            }
        }
        finally {
            serverSocket.close()
        }
    }
}

private class ServerTest(val clientSocket: Socket) : Runnable {
    private lateinit var className: String
    private lateinit var testMethod: String

    override fun run() {
        val input = ObjectInputStream(clientSocket.getInputStream())
        val output = ObjectOutputStream(clientSocket.getOutputStream())
        try {
            var message = input.readObject() as MessageHeader
            assert(message == MessageHeader.NEW_TEST, { "New test marker missed, but $message received" })
            className = input.readObject() as String
            testMethod = input.readObject() as String
            println("Preparing to execute test $className")

            message = input.readObject() as MessageHeader
            assert(message == MessageHeader.CLASS_PATH, { "Class path marker missed, but $message received" })
            val classPath = input.readObject() as Array<URL>

            val result = executeTest(URLClassLoader(classPath, JDK_EXT_JARS_CLASS_LOADER))
            output.writeObject(MessageHeader.RESULT)
            output.writeObject(result)
        }
        catch (e: Exception) {
            output.writeObject(MessageHeader.ERROR)
            output.writeObject(e)
        }
        finally {
            output.close()
            input.close()
            clientSocket.close()
        }
    }

    fun executeTest(classLoader: ClassLoader): String {
        val clazz = getGeneratedClass(classLoader, className)
        return getBoxMethodOrNull(clazz)!!.invoke(null) as String
    }

    companion object {
        //Required for org.jetbrains.kotlin.codegen.BlackBoxCodegenTestGenerated.FullJdk#testClasspath
        val JDK_EXT_JARS_CLASS_LOADER: ClassLoader
        init {
            val javaHome = System.getProperty("java.home")
            val extFolder = File(javaHome + "/lib/ext/")
            println(extFolder.canonicalPath)
            val listFiles = extFolder.listFiles()
            val additionalJars = listFiles?.filter { it.name.endsWith(".jar") }?.map { it.toURI().toURL() } ?: emptyList()
            JDK_EXT_JARS_CLASS_LOADER = URLClassLoader(additionalJars.toTypedArray(), null)
        }
    }
}