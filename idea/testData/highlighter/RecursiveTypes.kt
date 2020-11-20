// IGNORE_FIR
// EXPECTED_DUPLICATED_HIGHLIGHTING
// ISSUE: KT-42263

interface <info descr="null">TestModule</info>
<info descr="null">sealed</info> class <info descr="null">ResultingArtifact</info> {
    <info descr="null">abstract</info> class <info descr="null">Source</info><<info descr="null">R</info> : <info descr="null">Source</info><<info descr="null">R</info>>> : <info descr="null">ResultingArtifact</info>() {
        <info descr="null">abstract</info> val <info descr="null">frontendKind</info>: <info descr="null">FrontendKind</info><<info descr="null">R</info>>
    }
}
class <info descr="null">ClassicFrontendSourceArtifacts</info> : <info descr="null">ResultingArtifact</info>.<info descr="null">Source</info><<info descr="null">ClassicFrontendSourceArtifacts</info>>() {
    <info descr="null">override</info> val <info descr="null"><info descr="null">frontendKind</info></info>: <info descr="null">FrontendKind</info><<info descr="null">ClassicFrontendSourceArtifacts</info>>
        <info descr="null">get</info>() = <info descr="null">FrontendKind</info>.<info descr="null">ClassicFrontend</info>
}
<info descr="null">sealed</info> class <info descr="null">FrontendKind</info><<info descr="null">R</info> : <info descr="null">ResultingArtifact</info>.<info descr="null">Source</info><<info descr="null">R</info>>> {
    object <info descr="null">ClassicFrontend</info> : <info descr="null">FrontendKind</info><<info descr="null">ClassicFrontendSourceArtifacts</info>>()
}
<info descr="null">abstract</info> class <info descr="null">DependencyProvider</info> {
    <info descr="null">abstract</info> fun <<info descr="null">R</info> : <info descr="null">ResultingArtifact</info>.<info descr="null">Source</info><<info descr="null">R</info>>> <info descr="null">registerSourceArtifact</info>(<info descr="null">artifact</info>: <info descr="null">R</info>)
}
fun <info descr="null">test</info>(<info descr="null">dependencyProvider</info>: <info descr="null">DependencyProvider</info>, <info descr="null">artifact</info>: <info descr="null">ResultingArtifact</info>.<info descr="null">Source</info><*>) {
    <info descr="null">dependencyProvider</info>.<info descr="null">registerSourceArtifact</info>(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is ResultingArtifact.Source<*> but CapturedType(*) was expected">artifact</error>) // <- uncomment this and see exception
}
