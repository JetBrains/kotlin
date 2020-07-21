package org.jetbrains.kotlin.native.test.debugger

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import org.junit.Ignore
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

    /**
     * TODO: to enable this test it's required to fix issue with poisonig call site with wrong file owner ship of lambda
     * passed as parameter to inline function.
     */
    @Ignore
    @Test
    fun `address of VolatileLambda lookup`() = dwarfDumpComplexTest {
        val callbackLibrary = """
            ---
            int callback(int (*f)(int)) {
              return 42 + f(0xdeadbeef);
            }
        """.trimIndent().cinterop("callback", "callback")
        val trapLibrary = """
            ---
            void trap() {
              __builtin_trap();
            }
        """.trimIndent().cinterop("trap", "trap")

        val poisonLibrary = """
            package poison
            import trap.*
            import callback.*
            import kotlinx.cinterop.staticCFunction


            inline fun execute():Int {
              return callback(staticCFunction{
                a ->
                   trap()
                   2 * a
              })
            }
        """.trimIndent().library("poison", "-l", callbackLibrary.toString(), "-l", trapLibrary.toString())

        val binary = """
            import poison.*

            fun main() {
              execute()
            }
        """.trimIndent().binary("poisoned", "-g", "-l", poisonLibrary.toString(), "-l", callbackLibrary.toString(), "-l", trapLibrary.toString())

        /**
         * TODO: this has been address teken from lldb session:
         * (lldb) r
         * Process 25687 launched: '/var/folders/56/ldwtkqhx2t5g06z1s3tt9gth0000gp/T/dwarfdump_test_complex6078130564175092652/poisoned.kexe' (x86_64)
         * Process 25687 stopped
         *  * thread #1, queue = 'com.apple.main-thread', stop reason = EXC_BAD_INSTRUCTION (code=EXC_I386_INVOP, subcode=0x0)
         * frame #0: 0x0000000100092964 poisoned.kexe`trap_trap_wrapper0 + 4
         * poisoned.kexe`trap_trap_wrapper0:
         * ->  0x100092964 <+4>: ud2
         * 0x100092966:      nop
         * 0x100092967:      nop
         * 0x100092968:      nop
         * Target 0: (poisoned.kexe) stopped.
         * (lldb) bt
         *  * thread #1, queue = 'com.apple.main-thread', stop reason = EXC_BAD_INSTRUCTION (code=EXC_I386_INVOP, subcode=0x0)
         *  * frame #0: 0x0000000100092964 poisoned.kexe`trap_trap_wrapper0 + 4
         * frame #1: 0x000000010005403c poisoned.kexe`kfun:main$<anonymous>_1#internal(a=-559038737) at poisoned.kt:5:126
         * frame #2: 0x00000001000540f9 poisoned.kexe`_706f69736f6e6564_knbridge0(p0=-559038737) at poisoned.kt:5:93
         * frame #3: 0x0000000100092949 poisoned.kexe`callback_callback_wrapper0 + 25
         * frame #4: 0x0000000100053f47 poisoned.kexe`kfun:#main(){} [inlined] execute at poison.kt:8:10
         * frame #5: 0x0000000100053f3b poisoned.kexe`kfun:#main(){} at poisoned.kt:4
         * frame #6: 0x00000001000541b0 poisoned.kexe`Konan_start(args=0x0000000100309818) at poisoned.kt:3:1
         * frame #7: 0x000000010005edeb poisoned.kexe`Init_and_run_start + 107
         * frame #8: 0x00007fff700abcc9 libdyld.dylib`start + 1
         * (lldb)
         * On mac we can replace with calculating address with otool(1)
         *  > otool -tv -p '_kfun:main$<anonymous>_1#internal' ../poisoned.kexe
         * on other platforms with corresponding tools (objdump)
         */
        binary.dwarfDumpLookup(0x000000010005403c) {
            val subprogram = filter { it.tag == DwarfTag.Tag.DW_TAG_subprogram }.single() as DwarfTagSubprogram
            assertEquals(subprogram.file!!.name, "poison.kt")
        }
    }


}