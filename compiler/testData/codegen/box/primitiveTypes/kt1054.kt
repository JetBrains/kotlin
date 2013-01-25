fun box() : String {
  return if(true.and(true)) "OK" else "fail"
}

fun Boolean.and(other : Boolean) : Boolean{
  if(other == true)  {
    if(this == true){
      return true ;
     }
    else{
      return false;
    }
  }
  else {
    return false;
  }
}
