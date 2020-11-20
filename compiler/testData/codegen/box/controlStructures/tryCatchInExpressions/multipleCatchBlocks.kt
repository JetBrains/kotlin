// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
class Exception1(msg: String): Exception(msg)
class Exception2(msg: String): Exception(msg)
class Exception3(msg: String): Exception(msg)

fun box(): String =
        "O" + try {
            throw Exception3("K")
        }
        catch (e1: Exception1) {
            "e1"
        }
        catch (e2: Exception2) {
            "e2"
        }
        catch (e3: Exception3) {
            e3.message
        }
        catch (e: Exception) {
            "e"
        }
