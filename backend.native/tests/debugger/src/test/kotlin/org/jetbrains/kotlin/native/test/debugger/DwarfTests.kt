package org.jetbrains.kotlin.native.test.debugger

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class DwarfTests {
    @Test
    fun `prefix test`() = dwarfDumpTest("""
        fun main(args: Array<String>) {
            val xs = intArrayOf(3, 5, 8)
            return
        }

        data class Point(val x: Int, val y: Int)
    """.trimIndent(), listOf("-Xdebug-prefix-map=${System.getProperty("user.home")}=/xxx")){
        val map = flatMap( fun (it: DwarfTag): List<DwarfAttribute> { return it.attributes.values.toList()})
                .filter { it.attribute == DwarfAttribute.Attribute.DW_AT_decl_file }.map { it.rvString }
        assertNotSame(0, map.size)
    }
}