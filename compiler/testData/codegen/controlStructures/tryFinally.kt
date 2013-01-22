fun f(sb: StringBuilder, s: String): Unit {
  try {
    sb.append("foo");
    sb.append(Integer.parseInt(s));
  }
  finally {
    sb.append("bar");
  }
}