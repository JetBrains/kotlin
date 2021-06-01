// IGNORE_FIR
// INTENTION_TEXT: "Add import for 'pack.name.Fixtures.Register'"

package pack.name

class Fixtures {
    class Register {
        class Domain {
            object UserRepository {
                val authSuccess = true
                val authError = false
            }
        }
    }
}

fun test() {
    pack.name.Fixtures.Register<caret>.Domain.UserRepository.authSuccess
}