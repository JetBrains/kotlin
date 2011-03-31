package org.jetbrains.jet.lang.cfg;

/**
 * @author abreslav
 */
//public class JetControlFlowGraphBuilder extends AbstractControlFlowBuilder {
//
//    private final List<CFGNode> nodes = new ArrayList<CFGNode>();
//
//    private final List<CFGEdge> edges = new ArrayList<CFGEdge>();
//    private final Map<Label, CFGNode> labelsToNodes = new HashMap<Label, CFGNode>();
//    // toLabel -> edge
//    private final Map<Label, List<CFGEdge>> pendingEdges = new HashMap<Label, List<CFGEdge>>();
//
//    private List<Label> labelsToBeBound = new ArrayList<Label>();
//
//    private CFGNode currentNode;
//
//    public JetControlFlowGraphBuilder(JetExpression block) {
////        currentNode = createNode(block);
//    }
//
//    public void build(Pseudocode pseudocode) {
//        List<Instruction> instructions = pseudocode.getInstructions();
//        Map<Label, Integer> labels = pseudocode.getLabels();
//
//
//    }
//
//    @Override
//    public void exitSubroutine(JetElement element) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @NotNull
//    private CFGNode closest(List<CFGNode> nodes, int index) {
//        int size = nodes.size();
//        for (int i = index; i < size; i++) {
//            CFGNode cfgNode = nodes.get(i);
//            if (cfgNode != null) return cfgNode;
//        }
//        return null;
//    }
//
//    public void dumpGraph(PrintStream out) {
//        out.println("digraph g {");
//
//        Map<CFGNode, String> nodeToName = new HashMap<CFGNode, String>();
//        int count = 0;
//        for (CFGNode node : nodes) {
//            String name = "n" + count++;
//            nodeToName.put(node, name);
//            String text = node.getElement().getText();
//            int newline = text.indexOf("\n");
//            if (newline >= 0) {
//                text = text.substring(0, newline);
//            }
//            String shape = node instanceof CFGConditionNode ? "diamond" : "box";
//            out.println(name + "[label=\"" + text + "\", shape=" + shape + "];");
//        }
//
//        for (CFGEdge edge : edges) {
//            String from = nodeToName.get(edge.getFrom());
//            String to = nodeToName.get(edge.getTo());
//
//            String label = edge.getDebugLabel();
//            if (label != null) {
//                label = "[label=\"" + label + "\"]";
//            }
//            else {
//                label = "";
//            }
//            out.println(from + " -> " + to + label + ";");
//        }
//        out.println("}");
//        out.close();
//    }
//
//    private CFGNode createNode(JetExpression block) {
//        CFGNode node = new CFGReadNode(block);
//        afterNodeCreation(node);
//        return node;
//    }
//
//    private void afterNodeCreation(CFGNode node) {
//        nodes.add(node);
//        bindLabelIfNeeded(node);
//
//        if (currentNode != null) {
//            createEdgeIfNeeded(currentNode, node);
//        }
//
//        currentNode = node;
//    }
//
//    @Nullable
//    private CFGEdge createEdgeIfNeeded(@NotNull CFGNode from, CFGNode to) {
//        CFGEdge edge = new CFGEdge(from, to);
//        if (from instanceof CFGConditionNode) {
//            CFGConditionNode conditionNode = (CFGConditionNode) from;
//            if (conditionNode.getElseEdge() == null) {
//                conditionNode.setElseEdge(edge);
//            }
//            else if (conditionNode.getThenEdge() == null) {
//                conditionNode.setThenEdge(edge);
//            }
//            else {
//                return null;
//            }
//        }
//        else if (from instanceof CFGReadNode) {
//            CFGReadNode readNode = (CFGReadNode) from;
//            if (readNode.getOutgoingEdge() == null) {
//                readNode.setOutgoingEdge(edge);
//            }
//            else {
//                return readNode.getOutgoingEdge();
//            }
//        }
//        edges.add(edge);
//        return edge;
//    }
//
//    private void bindLabelIfNeeded(CFGNode node) {
//        for (Label label : labelsToBeBound) {
//            labelsToNodes.put(label, node);
//            List<CFGEdge> edges = pendingEdges.get(label);
//            if (edges != null) {
//                for (CFGEdge edge : edges) {
//                    edge.setTo(node);
//                }
//            }
//        }
//        labelsToBeBound.clear();
//    }
//
//    @Nullable
//    private CFGEdge createEdgeToLabel(CFGNode fromNode, Label toLabel) {
//        CFGNode toNode = labelsToNodes.get(toLabel);
//        if (toNode != null) {
//            return createEdgeIfNeeded(fromNode, toNode);
//        }
//        else {
//            List<CFGEdge> edges = pendingEdges.get(toLabel);
//            if (edges == null) {
//                edges = new ArrayList<CFGEdge>();
//                pendingEdges.put(toLabel, edges);
//            }
//            CFGEdge edge = createEdgeIfNeeded(fromNode,  null);
//            if (edge != null) {
//                edges.add(edge);
//            }
//            return edge;
//        }
//    }
//
//    @Override
//    public void readNode(JetExpression expression) {
//        createNode(expression);
//    }
//
//    @Override
//    public void jump(Label label) {
//        CFGNode target = labelsToNodes.get(label);
//        if (target != null) {
//            createEdgeIfNeeded(currentNode, target);
//        }
//        else {
//            createEdgeToLabel(currentNode, label);
//        }
//    }
//
//    @Override
//    public void bindLabel(Label label) {
//        assert !labelsToNodes.containsKey(label) : label;
//        labelsToBeBound.add(label);
//    }
//
//    @Override
//    public void jumpOnFalse(Label label) {
//        CFGConditionNode conditionNode = new CFGConditionNode(currentNode.getElement());
//        afterNodeCreation(conditionNode);
//        conditionNode.setElseEdge(createEdgeToLabel(conditionNode, label));
//    }
//
//    @Override
//    public void jumpOnTrue(Label label) {
//        CFGConditionNode conditionNode = new CFGConditionNode(currentNode.getElement());
//        afterNodeCreation(conditionNode);
//        conditionNode.setThenEdge(createEdgeToLabel(conditionNode, label));
//    }
//
//    @Override
//    public void createBoundLabel(JetSimpleNameExpression labelElement, JetExpression labeledExpression) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public Label getEntryPoint(JetSimpleNameExpression labelElement) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void jumpToLoopExitPoint(JetSimpleNameExpression labelElement) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public Label getCurrentSubroutineExitPoint() {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void returnValue(Label label) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void nondeterministicJump(Label firstBranch, Label secondBranch) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void exitTryFinally() {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void popCatchClauses() {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void pushCatchClauses(List<JetCatchClause> catchClauses) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void enterTryFinally(JetBlockExpression expression) {
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    @Override
//    public void unsupported(JetElement element) {
//        throw new IllegalStateException("Unsupported element: " + element.getText() + " " + element);
//    }
//
//}
//
//abstract class CFGNode {
//    private JetElement element;
//
//    public CFGNode(JetElement element) {
//        this.element = element;
//    }
//
//    @NotNull
//    public JetElement getElement() {
//        return element;
//    }
//
//    public void setElement(JetElement element) {
//        this.element = element;
//    }
//}
//
//class CFGReadNode extends CFGNode {
//    private CFGEdge outgoingEdge;
//
//    public CFGReadNode(JetElement element) {
//        super(element);
//    }
//
//    public CFGEdge getOutgoingEdge() {
//        return outgoingEdge;
//    }
//
//    public void setOutgoingEdge(CFGEdge outgoingEdge) {
//        this.outgoingEdge = outgoingEdge;
//    }
//}
//
//class CFGConditionNode extends CFGNode {
//
//    private CFGEdge thenEdge;
//    private CFGEdge elseEdge;
//
//    public CFGConditionNode(JetElement element) {
//        super(element);
//    }
//
//    public CFGConditionNode() {
//        super(null);
//    }
//
//    public CFGEdge getThenEdge() {
//        return thenEdge;
//    }
//
//    public void setThenEdge(CFGEdge thenEdge) {
//        this.thenEdge = thenEdge;
//        thenEdge.setDebugLabel("yes");
//    }
//
//    public CFGEdge getElseEdge() {
//        return elseEdge;
//    }
//
//    public void setElseEdge(CFGEdge elseEdge) {
//        this.elseEdge = elseEdge;
//        elseEdge.setDebugLabel("no");
//    }
//}
//
//class CFGEdge {
//    private CFGNode from;
//    private CFGNode to;
//    private String debugLabel = null;
//
//    public CFGEdge(CFGNode from, CFGNode to) {
//        this.from = from;
//        this.to = to;
//    }
//
//    public CFGNode getFrom() {
//        return from;
//    }
//
//    public void setFrom(CFGNode from) {
//        this.from = from;
//    }
//
//    public CFGNode getTo() {
//        return to;
//    }
//
//    public void setTo(CFGNode to) {
//        this.to = to;
//    }
//
//    public String getDebugLabel() {
//        return debugLabel;
//    }
//
//    public void setDebugLabel(String debugLabel) {
//        this.debugLabel = debugLabel;
//    }
//}