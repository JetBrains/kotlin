// FILE: a/x.java
package a;

public interface x {

    public class d {}

    public class z {}

    public class o {}
}

// FILE: a/b.java
package a;

public interface b extends x {

    public class y {}

}

// FILE: a/c.java
package a;

public class c implements x {

    public class d {}

}

// FILE: a/f.java
package a;

public class f extends c implements b {

    public class o {}

    public d getD() { return null; }
    public y getY() { return null; }
    public z getZ() { return null; }
    public o getO() { return null; }

}