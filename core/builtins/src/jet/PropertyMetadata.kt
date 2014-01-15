package jet

public trait PropertyMetadata {
    public val name: String
}

public class PropertyMetadataImpl(public override val name: String): PropertyMetadata
