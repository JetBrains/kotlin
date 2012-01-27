package org.jetbrains.jet.plugin;

/**
 * Configures jet parser for using indexed stubs instead of default set.
 * So if parser is used from IDEA it creates indexed stubs, if not direct psi elements are used.
 *
 * IMPORTANT: Should be called before loading parser.
 *
 * @author Nikolay Krasko
 */
public class ParserConfigurator {
//    static {
//        JetParserDefinition.setStubFactory(new JetIndexedStubElementFactory());
//    }
}
