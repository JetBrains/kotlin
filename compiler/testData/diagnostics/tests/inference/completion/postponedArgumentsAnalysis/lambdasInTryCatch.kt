// DIAGNOSTICS: -UNUSED_LAMBDA_EXPRESSION, -UNUSED_EXPRESSION

fun case1(x: Any){
    when (x){
        1 -> try { {"1"}; ""; 1} catch (e: Exception) { { }}
        "1" -> try { 1 } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }

    when (x){
        1 -> try { {"1"}; ""} catch (e: Exception) { { }}
        "1" -> try { 1 } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }
}

fun case2(x: Any){
    when (x){
        1 -> try { {"1"}; ""; TODO()} catch (e: Exception) { { }}
        "1" -> try { 1 } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }
    when (x){
        1 -> try { {"1"}; ""; TODO(); <!UNREACHABLE_CODE!>""<!>} catch (e: Exception) { { }}
        "1" -> try { 1 } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }
    when (x){
        1 -> try { {"1"}; ""; TODO()} catch (e: Exception) { { }}
        "1" -> try { 1; "" } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }
}

fun case3(x: Any){
    when (x){
        1 -> try { {"1"}} catch (e: Exception) { { }}
        "1" -> try { 1 } catch (e: Exception) { { }}
        else -> try { 1 } catch (e: Exception) { {1 }}
    }
}