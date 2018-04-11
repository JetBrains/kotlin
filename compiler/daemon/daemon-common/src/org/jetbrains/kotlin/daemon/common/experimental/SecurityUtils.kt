/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("CAST_NEVER_SUCCEEDS")

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteReadChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec


const val SECURITY_TOKEN_SIZE = 128

private val secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
private val pairGenerator = KeyPairGenerator.getInstance("DSA", "SUN")
private val keyFactory = KeyFactory.getInstance("DSA", "SUN")

private fun generateSecurityToken(): ByteArray {
    val tokenBuffer = ByteArray(SECURITY_TOKEN_SIZE)
    secureRandom.nextBytes(tokenBuffer)
    return tokenBuffer
}

data class SecurityData(val privateKey: PrivateKey, val publicKey: PublicKey, val token: ByteArray)
fun generateKeysAndToken() = pairGenerator.generateKeyPair().let {
    SecurityData(it.private, it.public, generateSecurityToken())
}

private fun FileInputStream.readAllBytes(): ByteArray {
    val bytes = arrayListOf<Byte>()
    val buffer = ByteArray(1024)
    var bytesRead = 0
    while (bytesRead != -1) {
        bytesRead = this.read(buffer, 0, 1024)
        bytes.addAll(buffer.toList())
    }
    return ByteArray(bytes.size, bytes::get)
}

private fun FileInputStream.readBytesFixedLength(n: Int): ByteArray {
    val buffer = ByteArray(n)
    var bytesRead = 0
    while (bytesRead != n) {
        bytesRead += this.read(buffer, bytesRead, n - bytesRead)
    }
    return buffer
}

// server part :
fun sendTokenKeyPair(output: FileOutputStream, token: ByteArray, privateKey: PrivateKey) {
    output.write(token)
    ObjectOutputStream(output).use {
        it.writeObject(privateKey)
    }
}

suspend fun getSignatureAndVerify(input: ByteReadChannelWrapper, expectedToken: ByteArray, publicKey: PublicKey): Boolean {
    val signature = input.nextBytes()
    val dsa = Signature.getInstance("SHA1withDSA", "SUN")
    dsa.initVerify(publicKey)
    dsa.update(expectedToken, 0, SECURITY_TOKEN_SIZE)
    val verified = dsa.verify(signature)
    log.info("verified : $verified")
    return verified
}


// client part :
fun readTokenKeyPairAndSign(input: FileInputStream): ByteArray {
    val token = input.readBytesFixedLength(SECURITY_TOKEN_SIZE)
    val privateKey = ObjectInputStream(input).use(ObjectInputStream::readObject) as PrivateKey
    val dsa = Signature.getInstance("SHA1withDSA", "SUN")
    dsa.initSign(privateKey)
    dsa.update(token, 0, SECURITY_TOKEN_SIZE)
    return dsa.sign()
}

suspend fun sendSignature(output: ByteWriteChannelWrapper, signature: ByteArray) = output.printBytesAndLength(signature.size, signature)