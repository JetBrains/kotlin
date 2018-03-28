/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.unit

import org.jetbrains.kotlin.daemon.common.experimental.CompilerServicesFacadeBaseClientSideImpl
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import org.jetbrains.kotlin.daemon.common.experimental.toRMI
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ClientSerializationTest : KotlinIntegrationTestBase() {

    fun testDefaultClient() {
        val file = createTempFile()
        val socketId = 1020
        val client = DefaultClient(socketId)
        println("created")
        file.outputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(client)
            }
        }
        println("printed")
        val client2 = file.inputStream().use {
            ObjectInputStream(it).use {
                it.readObject() as DefaultClient
            }
        }
        println("read")
        assert(client.serverPort == client2.serverPort)
        client.log.info("abacaba (1)")
        client2.log.info("abacaba (2)")
        println("test passed")
    }

    fun testCompilerServicesFacadeBaseClientSide() {
        val file = createTempFile()
        val socketId = 1020
        val client = CompilerServicesFacadeBaseClientSideImpl(socketId)
        println("created")
        file.outputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(client)
            }
        }
        println("printed")
        val client2 = file.inputStream().use {
            ObjectInputStream(it).use {
                it.readObject() as CompilerServicesFacadeBaseClientSideImpl
            }
        }
        println("read")
        assert(client.serverPort == client2.serverPort)
        println("test passed")
    }

    fun testRMIWrapper() {
        val file = createTempFile()
        val socketId = 1020
        val client = CompilerServicesFacadeBaseClientSideImpl(socketId).toRMI()
        println("created")
        file.outputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(client)
            }
        }
        println("printed")
        val client2 = file.inputStream().use {
            ObjectInputStream(it).use {
                it.readObject() as CompilerServicesFacadeBaseClientSideImpl
            }
        }
        println("read")
        println("test passed")
    }

}