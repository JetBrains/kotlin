// API_VERSION: LATEST

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

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 2 ISTORE 3
// 1 ISTORE 2
// 0 ILOAD 3
// 0 ILOAD 2
// 1 \$i\$f\$receiveJSON I .* 2
// 2 \$i\$f\$respond I .* 3

// JVM_IR_TEMPLATES
// 2 ISTORE 3
// 1 ISTORE 2
// 0 ILOAD 3
// 0 ILOAD 2
// 1 \$i\$f\$receiveJSON I .* 2
// 2 \$i\$f\$respond I .* 3
