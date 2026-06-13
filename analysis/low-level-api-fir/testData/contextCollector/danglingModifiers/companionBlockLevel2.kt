// LANGUAGE: +CompanionBlocksAndExtensions

package foo

annotation class Anno(val value: String)
const val FILE_CONSTANT = "file"

class Owner {
    @Anno("")
    companion {
        const val COMPANION_CONSTANT = "companion"

        @Anno(FILE_CONSTANT + <expr>COMPANION_CONSTANT</expr>)
    }
}
