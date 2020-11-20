interface ApplicationCall

interface AuthenticationService {
    suspend fun execute(request: Any): Any
}

suspend fun dummy() = Any()

suspend inline fun <reified Type : Any> ApplicationCall.receiveJSON(): Type {
    return dummy() as Type
}

suspend inline fun ApplicationCall.respond(message: Any) {
}

suspend fun ApplicationCall.test(authenticationService: AuthenticationService) {
    respond(authenticationService.execute(receiveJSON()))
}

// $i$f$respond x1, $i$f$receiveJSON x2: before and after suspension point
// 3 ISTORE 5
// 0 ILOAD 5
// 1 \$i\$f\$receiveJSON I .* 5
// 1 \$i\$f\$respond I .* 5
