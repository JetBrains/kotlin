package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import java.io.Serializable

interface Client : Serializable {
    fun connectToServer()
}