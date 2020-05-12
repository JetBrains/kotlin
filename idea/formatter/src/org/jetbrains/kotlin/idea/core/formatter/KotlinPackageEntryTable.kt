/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.formatter

import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.JDOMExternalizable
import org.jdom.Element

class KotlinPackageEntryTable: JDOMExternalizable, Cloneable {
    private val entries = mutableListOf<KotlinPackageEntry>()

    override fun equals(other: Any?): Boolean {
        if (other !is KotlinPackageEntryTable) return false
        if (other.entries.size != entries.size) return false

        return entries.zip(other.entries).any { (entry, otherEntry) -> entry != otherEntry }
    }

    public override fun clone(): KotlinPackageEntryTable {
        val clone = KotlinPackageEntryTable()
        clone.copyFrom(this)
        return clone
    }

    override fun hashCode(): Int {
        return entries.firstOrNull()?.hashCode() ?: 0
    }

    fun copyFrom(packageTable: KotlinPackageEntryTable) {
        entries.clear()
        entries.addAll(packageTable.entries)
    }

    fun getEntries(): Array<KotlinPackageEntry> {
        return entries.toTypedArray()
    }

    fun insertEntryAt(entry: KotlinPackageEntry, index: Int) {
        entries.add(index, entry)
    }

    fun removeEntryAt(index: Int) {
        entries.removeAt(index)
    }

    fun getEntryAt(index: Int): KotlinPackageEntry {
        return entries[index]
    }

    fun getEntryCount(): Int {
        return entries.size
    }

    fun setEntryAt(entry: KotlinPackageEntry, index: Int) {
        entries[index] = entry
    }

    operator fun contains(packageName: String): Boolean {
        for (entry in entries) {
            if (packageName.startsWith(entry.packageName)) {
                if (packageName.length == entry.packageName.length) return true
                if (entry.withSubpackages) {
                    if (packageName[entry.packageName.length] == '.') return true
                }
            }
        }
        return false
    }

    fun removeEmptyPackages() {
        entries.removeAll { it.packageName.isBlank() }
    }

    fun addEntry(entry: KotlinPackageEntry) {
        entries.add(entry)
    }

    override fun readExternal(element: Element) {
        entries.clear()

        element.children.forEach {
            if (it.name == "package") {
                val packageName = it.getAttributeValue("name") ?: throw InvalidDataException()
                val alias = it.getAttributeValue("alias")?.toBoolean() ?: false
                val withSubpackages = it.getAttributeValue("withSubpackages")?.toBoolean() ?: false

                val entry = when {
                    packageName.isEmpty() && !alias -> KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY
                    packageName.isEmpty() && alias -> KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
                    else -> KotlinPackageEntry(packageName, withSubpackages)
                }

                entries.add(entry)
            }
        }
    }

    override fun writeExternal(parentNode: Element) {
        for (entry in entries) {
            val element = Element("package")
            parentNode.addContent(element)
            val name = if (entry == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY || entry == KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY) "" else entry.packageName
            val alias = (entry == KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY)

            element.setAttribute("name", name)
            element.setAttribute("alias", alias.toString())
            element.setAttribute("withSubpackages", entry.withSubpackages.toString())
        }
    }
}