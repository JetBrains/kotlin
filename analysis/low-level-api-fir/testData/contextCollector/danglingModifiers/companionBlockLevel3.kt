// LANGUAGE: +CompanionBlocksAndExtensions

package foo

annotation class Anno(val value: String)
const val FILE_CONSTANT = "file"

class Owner {
    @Anno(<expr>FILE_CONSTANT</expr>)
    companion {
        @Anno("")
    }
}
