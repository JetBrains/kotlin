package my.ada.adad.ad

import my.ada.adad.ad.Fixtures.Register.Domain.UserRepository

object Fixtures {
    object Register {
        object Domain {
            object UserRepository {
                const val authSuccess = true
            }
        }
    }
}

fun test() {
    my.ada.adad.ad.Fixtures.Register.Domain<caret>.UserRepository.authSuccess
}