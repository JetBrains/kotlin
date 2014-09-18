IN = open("list.txt").readlines()
OUT = open("result.txt", "w")

for line in IN:
  line = line.split()[0]
  if line[-2:] == "kt":
    filename = line[:-3]
    if 'Error' not in '\n'.join(open("psi/" + filename + ".txt").readlines()):	   
        OUT.write("public void test%s() { doTest(true); }\n" % filename)

OUT.close()

