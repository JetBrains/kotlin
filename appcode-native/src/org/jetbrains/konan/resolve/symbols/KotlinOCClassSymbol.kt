/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.util.Processor
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.cidr.lang.symbols.OCQualifiedName
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCVisibility
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCImplementationSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.types.OCObjectType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass


abstract class KotlinOCClassSymbol<Stub : ObjCClass<*>>(stub: Stub, project: Project) : KotlinOCWrapperSymbol<Stub>(stub, project),
    OCClassSymbol {

    private var myMembers: MostlySingularMultiMap<String, OCMemberSymbol>? = null
    private val myQualifiedName = OCQualifiedName.interned(name)

    override fun isGlobal(): Boolean = true

    override fun getProtocolNames(): List<String> = stub.superProtocols

    override fun getMembersCount(): Int {
        return myMembers?.size() ?: 0
    }

    override fun <T : OCMemberSymbol?> processMembers(
        memberName: String?,
        memberClass: Class<out T>?,
        processor: Processor<in T>
    ): Boolean {
        val members = myMembers
        if (members == null) return true

        val myProcessor: Processor<OCMemberSymbol> = if (memberClass != null) {
            Processor { member -> !memberClass.isAssignableFrom(member.javaClass) || processor.process(member as T) }
        } else {
            processor as Processor<OCMemberSymbol>
        }

        return if (memberName == null) {
            members.processAllValues(myProcessor)
        } else {
            members.processForKey(memberName, myProcessor)
        }
    }

    override fun getDefinitionSymbol(project: Project): OCClassSymbol? = this

    override fun getInterfaceOrProtocol(project: Project): OCClassSymbol? = this

    override fun getImplementation(project: Project): OCImplementationSymbol? = null

    override fun getResolvedType(context: OCResolveContext, ignoringImports: Boolean): OCObjectType? =
        type.resolve(context, ignoringImports) as? OCObjectType

    fun setMembers(members: Sequence<OCMemberSymbol>) {
        //we don't want to store empty map, so let's initialize it only if there're any members
        var map: MostlySingularMultiMap<String, OCMemberSymbol>? = null
        for (member in members) {
            if (map == null) map = MostlySingularMultiMap()
            map.add(member.name, member)
        }
        myMembers = map
    }

    override fun getParent(): OCSymbolWithQualifiedName? = null

    override fun getNameWithParent(context: OCResolveContext): String = name

    override fun getVisibility(): OCVisibility? = null

    override fun getQualifiedName(): OCQualifiedName = myQualifiedName

    override fun dropSubstitution(): OCSymbol = this
}