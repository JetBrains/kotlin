/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.unit

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.*
import org.jetbrains.kotlin.daemon.common.experimental.CompilerServicesFacadeBaseClientSideImpl
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterfaceKtor.selectorMgr
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ServerBase
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openIO
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.runWithTimeout
import org.jetbrains.kotlin.daemon.common.toRMI
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.util.logging.Logger

class TestServer(val serverPort: Int = 6999) {
    private val serverSocket = aSocket(selectorMgr).tcp().bind(InetSocketAddress(serverPort))
    private val log = Logger.getLogger("TestServer")

    fun awaitClient() = GlobalScope.async {
        log.info("accepting clientSocket...")
        val client = serverSocket.accept()
        log.info("client accepted! (${client.remoteAddress})")
        val (input, output) = client.openIO(log)

        true
    }
}

val testServer = TestServer()

@Suppress("UNCHECKED_CAST")
class ClientSerializationTest : KotlinIntegrationTestBase() {

    val file = createTempFile()

    val log = Logger.getLogger("ClientSerializationTest")

    private inline fun <reified T> abstractSerializationTest(initClient: () -> T, vararg additionalTests: (T, T) -> Unit) {
        val client = initClient()
        log.info("created")
        file.outputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(client)
            }
        }
        log.info("printed")
        var client2: T? = null
        var connected = false
        runBlocking {
            val clientAwait = testServer.awaitClient()
            client2 = file.inputStream().use {
                ObjectInputStream(it).use {
                    it.readObject() as T
                }
            }
            connected = runWithTimeout { clientAwait.await() } ?: false
        }
        assert(connected)
        log.info("read")
        assert(client2 != null)
        additionalTests.forEach { it(client, client2!!) }
        log.info("test passed")
    }

    fun testDefaultClient() = abstractSerializationTest(
        { DefaultClient<ServerBase>(testServer.serverPort) },
        { client, client2 -> assert(client.serverPort == client2.serverPort) },
        { client, client2 ->
            client2.log.info("abacaba (2)")
            log.info("test passed")
        }
    )

    fun testCompilerServicesFacadeBaseClientSide() = abstractSerializationTest(
        { CompilerServicesFacadeBaseClientSideImpl(testServer.serverPort) },
        { client, client2 -> assert(client.serverPort == client2.serverPort) }
    )

    fun testRMIWrapper() = abstractSerializationTest({ CompilerServicesFacadeBaseClientSideImpl(testServer.serverPort).toRMI() })

}