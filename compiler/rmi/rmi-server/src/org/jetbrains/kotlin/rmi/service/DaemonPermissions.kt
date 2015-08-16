/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.rmi.service

import java.io.FilePermission
import java.net.SocketPermission
import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.Policy
import java.util.ArrayList
import java.util.Collections
import java.util.Enumeration
import java.util.PropertyPermission


public class DaemonPolicy(val port: Int) : Policy() {

    private fun createPermissions(): PermissionCollection {
        val perms = DaemonPermissionCollection()

        val socketPermission = SocketPermission("localhost:$port-", "connect, accept, resolve")
        val propertyPermission = PropertyPermission("localhost:$port", "read")
        //val filePermission = FilePermission("<<ALL FILES>>", "read")

        perms.add(socketPermission)
        perms.add(propertyPermission)
        //perms.add(filePermission)

        return perms
    }

    private val perms: PermissionCollection by lazy { createPermissions() }

    override fun getPermissions(codesource: CodeSource?): PermissionCollection {
        return perms
    }
}


class DaemonPermissionCollection : PermissionCollection() {
    var perms = ArrayList<Permission>()

    override fun add(p: Permission) {
        perms.add(p)
    }

    override fun implies(p: Permission): Boolean {
        val i = perms.iterator()
        while (i.hasNext()) {
            if (i.next().implies(p)) {
                return true
            }
        }
        return false
    }

    override fun elements(): Enumeration<Permission> {
        return Collections.enumeration(perms)
    }

    override fun isReadOnly(): Boolean {
        return false
    }
//
//    companion object {
//
//        private val serialVersionUID = 614300921365729272L
//    }

}


public fun setDaemonPpermissions(port: Int) {
    Policy.setPolicy(DaemonPolicy(port))
}

