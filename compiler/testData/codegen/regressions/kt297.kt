// KT-297 Overload resolution ambiguity with required in interface
interface ALE<T> : java.util.ArrayList<T> {
   fun getOrValue(index: Int, value : T) : T = if(index >= 0 && index < size()) get(index) else value
}