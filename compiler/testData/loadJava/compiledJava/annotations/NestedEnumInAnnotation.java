// FILE: NestedEnumInAnnotation.java

package test;

@Api(status = Api.Status.Ok)
public class NestedEnumInAnnotation {}

// FILE: Api.java
package test;

public @interface Api {
    Status status();

    enum Status {
        Ok, Error;
    }
}
